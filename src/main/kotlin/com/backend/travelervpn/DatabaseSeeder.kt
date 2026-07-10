import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.repository.VpnUserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DatabaseSeeder(vpnUserRepository: VpnUserRepository) : CommandLineRunner {
    private val vpnUserRepository: VpnUserRepository;

    init {
        this.vpnUserRepository = vpnUserRepository
    }

    override fun run(vararg args: String) {
        suspend {
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
}