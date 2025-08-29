package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class SyncResult(
    val totalUsers: Int,
    val successCount: Int,
    val errorCount: Int,
    val errors: List<String>
)
