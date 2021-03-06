package com.bridgelabz.bookstoreapi.entity;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name="cart_details")
public class CartDetails {

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long cartId;
	
	@Column(name = "placed_timed", nullable = false)
	private LocalDateTime placeTime;

	@OneToMany(cascade = CascadeType.ALL, targetEntity = Quantity.class)
	@JoinColumn(name = "cartId")
	private List<Quantity> QuantityOfBooks;
	
	@ManyToMany(cascade = CascadeType.ALL)
	private List<Book> BooksList;

	
	public CartDetails() {
		super();
	}


	public Long getCartId() {
		return cartId;
	}


	public void setCartId(Long cartId) {
		this.cartId = cartId;
	}


	public LocalDateTime getPlaceTime() {
		return placeTime;
	}



	public void setPlaceTime(LocalDateTime placeTime) {
		this.placeTime = placeTime;
	}



	public List<Quantity> getQuantityOfBooks() {
		return QuantityOfBooks;
	}



	public void setQuantityOfBooks(List<Quantity> quantityOfBooks) {
		QuantityOfBooks = quantityOfBooks;
	}



	public List<Book> getBooksList() {
		return BooksList;
	}



	public void setBooksList(List<Book> booksList) {
		BooksList = booksList;
	}
	
}
