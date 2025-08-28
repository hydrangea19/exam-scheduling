package mk.ukim.finki.examscheduling.usermanagement.query

data class UserPageResponse(
    val users: List<UserView>,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalElements: Long
)
