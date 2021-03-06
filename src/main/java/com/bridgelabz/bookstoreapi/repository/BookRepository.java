package com.bridgelabz.bookstoreapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bridgelabz.bookstoreapi.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>{

	@Query(value = "select * from books as b where b.book_verified=true limit :start,12", 
			nativeQuery = true)
	public List<Book> findBook(@Param("start") Integer start);
	
	@Query(value = "select * from books as b where b.book_verified=true order by b.book_price limit :start,12", 
			nativeQuery = true)
	public List<Book> findBookSortedByPriceLow(@Param("start") Integer start);
	
	@Query(value = "select * from books as b where b.book_verified=true order by b.book_price desc limit :start,12", 
			nativeQuery = true)
	public List<Book> findBookSortedByPriceHigh(@Param("start") Integer start);
	
	@Query(value = "select * from books as b where b.book_verified=true order by b.book_added_time limit :start,12", 
			nativeQuery = true)
	public List<Book> findBookSortedByArrival(@Param("start") Integer start);
	
	@Query(value = "select * from books as b where b.book_verified=true", 
			nativeQuery = true)
	public List<Book> findAllBook();

	@Query(value = "select * from books as b where b.book_verified=false", 
			nativeQuery = true)
	public List<Book> findUnverifiedBook();
	
	@Query(value = "select * from books as b where b.book_unapprove=true", 
			nativeQuery = true)
	public List<Book> findUnApproveBook();
	
	@Query(value = "select * from books where seller_id=:sellerId limit :start,12", 
			nativeQuery = true)
	public List<Book> findSellerBook(@Param("start") Integer start, @Param("sellerId") Long sellerId);
}