package adminServer.mvp.user;
import java.time.Instant; import java.util.*; import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid; import jakarta.validation.constraints.NotNull;
@RestController @RequestMapping("/api/users") public class UserController {
 private final UserRepository users; public UserController(UserRepository users){this.users=users;}
 @GetMapping public List<UserView> list(){return users.findAll().stream().map(UserView::from).toList();}
 @PatchMapping("/{id}/role") public UserView role(@PathVariable UUID id,@Valid @RequestBody RoleRequest request){User user=users.findById(id).orElseThrow();if(user.getRole()==UserRole.ADMIN&&request.role()!=UserRole.ADMIN&&users.findAll().stream().filter(u->u.getRole()==UserRole.ADMIN).count()<=1)throw new IllegalStateException("A instalação precisa manter ao menos um administrador");user.setRole(request.role());return UserView.from(users.save(user));}
 public record RoleRequest(@NotNull UserRole role){} public record UserView(UUID id,String username,UserRole role,Instant createdAt){static UserView from(User u){return new UserView(u.getId(),u.getUsername(),u.getRole(),u.getCreatedAt());}}
}
