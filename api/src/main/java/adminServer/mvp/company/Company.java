package adminServer.mvp.company;
import java.time.Instant; import java.util.UUID; import adminServer.mvp.user.User; import jakarta.persistence.*;
@Entity @Table(name="companies") public class Company {@Id private UUID id;private String name;@ManyToOne(fetch=FetchType.LAZY)@JoinColumn(name="created_by")private User createdBy;@Column(name="created_at")private Instant createdAt;protected Company(){}public Company(String n,User u){id=UUID.randomUUID();name=n;createdBy=u;createdAt=Instant.now();}public UUID getId(){return id;}public String getName(){return name;}}
