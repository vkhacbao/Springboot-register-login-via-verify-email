package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.User;

@Repository("userRepository")
public  interface  UserRepository extends JpaRepository<User, String>{
	User findByEmailIdIgnoreCase(String emailId);
	
	
    @Query(value = "select * from user u where u.is_enabled = ?1", nativeQuery = true)
    User getIsEnabled(int isEnabled);

	

}
