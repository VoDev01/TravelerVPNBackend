import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.repository.VpnUserRepository
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DatabaseSeeder(private val vpnUserRepository: VpnUserRepository) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (vpnUserRepository.count() == 0L) {
            val client = VpnUser(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "http://localhost:8080"
            )
            vpnUserRepository.save(client)
            println("Database successfully seeded!")
        }
    }
}