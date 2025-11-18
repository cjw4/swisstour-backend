package dg.swiss.swiss_dg_db.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomUserRepository extends JpaRepository<CustomUser, String> {

}
