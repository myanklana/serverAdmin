package adminServer.mvp.alerts;
import java.util.*; import org.springframework.data.jpa.repository.*;
public interface AlertRuleRepository extends JpaRepository<AlertRule,UUID>{List<AlertRule> findByServerIdAndEnabledTrue(UUID serverId);@Query("select r from AlertRule r join CompanyMember m on m.company=r.server.company where m.user.id=:ownerId order by r.server.name")List<AlertRule> findByServerOwnerIdOrderByServerNameAsc(UUID ownerId);}
