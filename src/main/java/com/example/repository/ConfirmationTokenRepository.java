package com.example.repository;

import org.springframework.data.repository.CrudRepository;

import com.example.ConfirmationToken;

public interface  ConfirmationTokenRepository extends CrudRepository<ConfirmationToken, String>{
	ConfirmationToken findByConfirmationToken (String confirmationToken);

}
