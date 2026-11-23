package com.example.data

import android.content.Context
import com.example.R
import com.example.model.GatheringAlbum
import com.example.model.GatheringPhoto
import com.example.model.MomentTag
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HearthRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  suspend fun ensureUserProfile(displayName: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docPath = "users/$uid"
    return try {
      val cleanName = displayName.trim().ifBlank { "Family Member" }.take(60)
      val docRef = db.collection("users").document(uid)
      val existing = docRef.get().await()
      if (!existing.exists()) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "displayName" to cleanName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else if (existing.getString("displayName") != cleanName) {
        docRef.update(
          mapOf(
            "displayName" to cleanName,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  fun observeMyAlbums(userId: String): Flow<List<GatheringAlbum>> = flow {
    val path = "albums"
    emitAll(
      db.collection(path)
        .whereArrayContains("memberUids", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              GatheringAlbum::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeAlbumPhotos(albumId: String, userId: String): Flow<List<GatheringPhoto>> = flow {
    val path = "albums/$albumId/photos"
    emitAll(
      db.collection("albums")
        .document(albumId)
        .collection("photos")
        .whereArrayContains("memberUids", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              GatheringPhoto::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.takenAtMillis }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createAlbum(
    title: String,
    gatheringMonth: String,
    hostName: String,
  ): Result<GatheringAlbum> {
    var docPath = "albums"
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim()
      val cleanMonth = gatheringMonth.trim()
      val cleanHost = hostName.trim().ifBlank { "Family Host" }.take(60)
      require(cleanTitle.length in 2..80) { "Album title must be 2 to 80 characters." }
      require(cleanMonth.length in 2..40) { "Gathering month must be 2 to 40 characters." }

      val inviteCode = generateInviteCode()
      val albumRef = db.collection("albums").document()
      val inviteRef = db.collection("invites").document(inviteCode)
      docPath = albumRef.path

      val batch = db.batch()
      batch.set(
        albumRef,
        mapOf(
          "hostUserId" to uid,
          "hostName" to cleanHost,
          "title" to cleanTitle,
          "gatheringMonth" to cleanMonth,
          "inviteCode" to inviteCode,
          "memberUids" to listOf(uid),
          "photoCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        inviteRef,
        mapOf(
          "albumId" to albumRef.id,
          "hostUserId" to uid,
          "albumTitle" to cleanTitle,
          "gatheringMonth" to cleanMonth,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()

      Result.success(
        GatheringAlbum(
          id = albumRef.id,
          hostUserId = uid,
          hostName = cleanHost,
          title = cleanTitle,
          gatheringMonth = cleanMonth,
          inviteCode = inviteCode,
          memberUids = listOf(uid),
          photoCount = 0L,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun joinAlbumByInviteCode(rawCode: String): Result<String> {
    var docPath = "invites"
    return try {
      val uid = requireUserId()
      val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
      require(code.length in 4..12) { "Enter a valid 4 to 12 character invite code." }

      docPath = "invites/$code"
      val inviteSnap = db.collection("invites").document(code).get().await()
      if (!inviteSnap.exists()) {
        return Result.failure(IllegalArgumentException("No gathering found for code $code."))
      }
      val albumId =
        inviteSnap.getString("albumId")
          ?: return Result.failure(IllegalStateException("Invite is missing an album reference."))

      // Check if user is already a member in their active albums query
      val alreadyMember =
        db.collection("albums")
          .whereArrayContains("memberUids", uid)
          .get()
          .await()
          .documents
          .any { it.id == albumId }

      if (alreadyMember) {
        return Result.success(albumId)
      }

      docPath = "albums/$albumId"
      db.collection("albums")
        .document(albumId)
        .update(
          mapOf(
            "memberUids" to FieldValue.arrayUnion(uid),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      Result.success(albumId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun uploadPhoto(
    album: GatheringAlbum,
    uploaderName: String,
    encoded: EncodedPhoto,
    caption: String,
    momentTag: MomentTag,
  ): Result<Unit> {
    var docPath = "albums/${album.id}/photos"
    return try {
      val uid = requireUserId()
      require(album.id.isNotBlank()) { "Album ID must not be blank." }
      val cleanUploader = uploaderName.trim().ifBlank { "Family Member" }.take(60)
      val cleanCaption = caption.trim().take(160)
      val safeMembers =
        (album.memberUids + uid).distinct().take(50)

      val albumRef = db.collection("albums").document(album.id)
      val photoRef = albumRef.collection("photos").document()
      docPath = photoRef.path

      val batch = db.batch()
      batch.set(
        photoRef,
        mapOf(
          "albumId" to album.id,
          "uploaderId" to uid,
          "uploaderName" to cleanUploader,
          "caption" to cleanCaption,
          "momentTag" to momentTag.name,
          "dayLabel" to encoded.dayLabel.ifBlank { "November" }.take(40),
          "takenAtMillis" to encoded.takenAtMillis,
          "imageBase64" to encoded.imageBase64,
          "aspectRatio" to encoded.aspectRatio,
          "starredByUids" to emptyList<String>(),
          "memberUids" to safeMembers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        albumRef,
        mapOf(
          "photoCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleStarPhoto(
    albumId: String,
    photo: GatheringPhoto,
    userId: String,
  ): Result<Unit> {
    val docPath = "albums/$albumId/photos/${photo.id}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(albumId.isNotBlank() && photo.id.isNotBlank()) { "Photo reference missing." }

      val updatedStars =
        if (uid in photo.starredByUids) {
          photo.starredByUids.filterNot { it == uid }
        } else {
          (photo.starredByUids + uid).distinct().take(50)
        }

      db.collection("albums")
        .document(albumId)
        .collection("photos")
        .document(photo.id)
        .update(
          mapOf(
            "starredByUids" to updatedStars,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updatePhotoDetails(
    albumId: String,
    photoId: String,
    caption: String,
    momentTag: MomentTag,
  ): Result<Unit> {
    val docPath = "albums/$albumId/photos/$photoId"
    return try {
      requireUserId()
      require(albumId.isNotBlank() && photoId.isNotBlank()) { "Photo reference missing." }
      val cleanCaption = caption.trim().take(160)
      db.collection("albums")
        .document(albumId)
        .collection("photos")
        .document(photoId)
        .update(
          mapOf(
            "caption" to cleanCaption,
            "momentTag" to momentTag.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deletePhoto(album: GatheringAlbum, photoId: String): Result<Unit> {
    val docPath = "albums/${album.id}/photos/$photoId"
    return try {
      requireUserId()
      require(album.id.isNotBlank() && photoId.isNotBlank()) { "Photo reference missing." }
      val albumRef = db.collection("albums").document(album.id)
      val photoRef = albumRef.collection("photos").document(photoId)

      val batch = db.batch()
      batch.delete(photoRef)
      if (album.photoCount > 0L) {
        batch.update(
          albumRef,
          mapOf(
            "photoCount" to FieldValue.increment(-1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun syncMissingMemberUids(
    album: GatheringAlbum,
    photos: List<GatheringPhoto>,
  ): Result<Unit> {
    val docPath = "albums/${album.id}/photos"
    return try {
      val uid = requireUserId()
      if (uid !in album.memberUids) return Result.success(Unit)
      val albumMembers = album.memberUids.take(50)
      val outdatedPhotos =
        photos.filter { photo ->
          uid in photo.memberUids && !photo.memberUids.containsAll(albumMembers)
        }
      if (outdatedPhotos.isEmpty()) return Result.success(Unit)

      val albumRef = db.collection("albums").document(album.id)
      outdatedPhotos.chunked(20).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { photo ->
          val merged = (photo.memberUids + albumMembers).distinct().take(50)
          batch.update(
            albumRef.collection("photos").document(photo.id),
            mapOf(
              "memberUids" to merged,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  private fun generateInviteCode(): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val rng = SecureRandom()
    val suffix = (1..4).map { alphabet[rng.nextInt(alphabet.length)] }.joinToString("")
    return "NOV$suffix"
  }
}
