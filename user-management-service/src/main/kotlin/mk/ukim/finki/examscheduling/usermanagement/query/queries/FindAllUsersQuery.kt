package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindAllUsersQuery(
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "fullName",
    val sortDirection: String = "ASC"
)