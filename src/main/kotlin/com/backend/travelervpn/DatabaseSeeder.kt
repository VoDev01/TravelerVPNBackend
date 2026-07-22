import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.repository.VpnUserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class DatabaseSeeder(private val vpnUserRepository: VpnUserRepository) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (vpnUserRepository.count() == 0L) {
            val datePrefix = OffsetDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd"))
            val shortUuid = UUID.randomUUID().toString()
                .replace("-", "").take(6)

            val username = "vpn_${datePrefix}_$shortUuid"

            val client = VpnUser(
                userId = UUID.randomUUID(),
                username = username,
            )
            vpnUserRepository.save(client)
            println("Database successfully seeded!")
        }
    }
}