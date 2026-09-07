package adminServer.mvp.audit; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,UUID>{List<AuditLog> findTop200ByOrderByCreatedAtDesc();}
