package adminServer.mvp.company;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface CompanyMemberRepository extends JpaRepository<CompanyMember,UUID>{Optional<CompanyMember> findByCompanyIdAndUserId(UUID company,UUID user);boolean existsByCompanyIdAndUserId(UUID company,UUID user);}
