package com.example.smartsitiesbot.repositories;

import com.example.smartsitiesbot.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
