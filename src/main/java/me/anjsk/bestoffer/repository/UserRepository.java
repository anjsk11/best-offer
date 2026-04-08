package me.anjsk.bestoffer.repository;

import me.anjsk.bestoffer.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
}
