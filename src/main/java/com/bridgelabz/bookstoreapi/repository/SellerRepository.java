package com.bridgelabz.bookstoreapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgelabz.bookstoreapi.entity.Book;
import com.bridgelabz.bookstoreapi.entity.Seller;

public interface SellerRepository extends JpaRepository<Seller, Long>{

	Optional<Seller> findByEmail(String email);
	Optional<Seller> findByMobile(Long mobile);
	@Query(value=" select * from seller where email=?",nativeQuery=true)
	Seller getUser(String email);
	@Query(value=" select * from seller",nativeQuery=true)
	List<Seller> getSellers();
	@Query(value=" select * from seller where seller_id=?",nativeQuery=true)
	Seller findSellerById(Long id);
	
}
