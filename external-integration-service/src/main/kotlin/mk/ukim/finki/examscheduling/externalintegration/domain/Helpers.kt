package mk.ukim.finki.examscheduling.externalintegration.domain

import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourse
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamRoom
import java.time.Duration
import java.time.Instant

fun ExamCourse.isStale(): Boolean {
    return Duration.between(lastUpdated, Instant.now()).toHours() > 24
}

fun ExamProfessor.isStale(): Boolean {
    return Duration.between(lastUpdated, Instant.now()).toHours() > 12
}

fun ExamRoom.isStale(): Boolean {
    return Duration.between(lastUpdated, Instant.now()).toHours() > 12
}