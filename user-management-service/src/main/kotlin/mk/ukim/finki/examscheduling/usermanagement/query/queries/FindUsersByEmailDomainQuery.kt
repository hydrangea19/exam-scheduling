package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindUsersByEmailDomainQuery(
    val domain: String,
    val activeOnly: Boolean = true,
    val page: Int = 0,
    val size: Int = 20
)