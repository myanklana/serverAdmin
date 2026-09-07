package adminServer.mvp.company;import java.util.*;import org.springframework.data.jpa.repository.*;
public interface CompanyRepository extends JpaRepository<Company,UUID>{@Query("select c from Company c join CompanyMember m on m.company=c where m.user.id=:userId order by c.name")List<Company> findAccessible(UUID userId);}
