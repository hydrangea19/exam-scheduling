package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class SearchUsersByNameQuery(
    val namePattern: String,
    val activeOnly: Boolean = true,
    val page: Int = 0,
    val size: Int = 20
)