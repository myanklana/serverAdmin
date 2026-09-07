package adminServer.mvp.audit;
import java.time.Instant; import java.util.UUID; import adminServer.mvp.server.ManagedServer; import adminServer.mvp.user.User; import jakarta.persistence.*;
@Entity @Table(name="audit_logs") public class AuditLog {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user; private String username; private String action; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="server_id") private ManagedServer server; @Column(name="source_ip") private String sourceIp; @Column(columnDefinition="text") private String details; @Column(name="created_at") private Instant createdAt;
 protected AuditLog(){} public AuditLog(User u,String a,ManagedServer s,String ip,String d){id=UUID.randomUUID();user=u;username=u.getUsername();action=a;server=s;sourceIp=ip;details=d;createdAt=Instant.now();}
 public UUID getId(){return id;} public String getUsername(){return username;} public String getAction(){return action;} public ManagedServer getServer(){return server;} public String getSourceIp(){return sourceIp;} public String getDetails(){return details;} public Instant getCreatedAt(){return createdAt;}
}
