package mk.ukim.finki.examscheduling.externalintegration.domain

import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamRoom
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class AvailabilityDataCache(
    private val cacheManager: CacheManager
) {
    private val logger = LoggerFactory.getLogger(AvailabilityDataCache::class.java)
    private val professorCache = cacheManager.getCache("professors")
    private val roomCache = cacheManager.getCache("rooms")
    private val allProfessorsCache = cacheManager.getCache("all-professors")
    private val allRoomsCache = cacheManager.getCache("all-rooms")
    private var lastProfessorCacheUpdate: Instant = Instant.MIN
    private var lastRoomCacheUpdate: Instant = Instant.MIN

    fun getProfessor(professorId: String): ExamProfessor? {
        return professorCache?.get(professorId, ExamProfessor::class.java)
    }

    fun cacheProfessor(professor: ExamProfessor) {
        professorCache?.put(professor.professorId, professor)
        logger.debug("Cached professor: {}", professor.professorId)
    }

    fun getAllProfessors(): List<ExamProfessor> {
        return allProfessorsCache?.get("all", List::class.java) as? List<ExamProfessor> ?: emptyList()
    }

    fun cacheAllProfessors(professors: List<ExamProfessor>) {
        allProfessorsCache?.put("all", professors)
        professors.forEach { cacheProfessor(it) }
        lastProfessorCacheUpdate = Instant.now()
        logger.info("Cached {} professors", professors.size)
    }

    fun isProfessorCacheStale(): Boolean {
        return Duration.between(lastProfessorCacheUpdate, Instant.now()).toHours() > 4
    }

    fun getRoom(roomId: String): ExamRoom? {
        return roomCache?.get(roomId, ExamRoom::class.java)
    }

    fun cacheRoom(room: ExamRoom) {
        roomCache?.put(room.roomId, room)
        logger.debug("Cached room: {}", room.roomId)
    }

    fun getAllRooms(): List<ExamRoom> {
        return allRoomsCache?.get("all", List::class.java) as? List<ExamRoom> ?: emptyList()
    }

    fun cacheAllRooms(rooms: List<ExamRoom>) {
        allRoomsCache?.put("all", rooms)
        rooms.forEach { cacheRoom(it) }
        lastRoomCacheUpdate = Instant.now()
        logger.info("Cached {} rooms", rooms.size)
    }

    fun isRoomCacheStale(): Boolean {
        return Duration.between(lastRoomCacheUpdate, Instant.now()).toHours() > 4
    }

    fun clearCache() {
        professorCache?.clear()
        roomCache?.clear()
        allProfessorsCache?.clear()
        allRoomsCache?.clear()
        lastProfessorCacheUpdate = Instant.MIN
        lastRoomCacheUpdate = Instant.MIN
        logger.info("Availability cache cleared")
    }
}