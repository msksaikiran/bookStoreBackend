package com.bridgelabz.bookstoreapi.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.bridgelabz.bookstoreapi.configuration.Consumer;
import com.bridgelabz.bookstoreapi.configuration.Producer;
import com.bridgelabz.bookstoreapi.constants.Constants;
import com.bridgelabz.bookstoreapi.dto.BookDTO;
import com.bridgelabz.bookstoreapi.dto.Mail;
import com.bridgelabz.bookstoreapi.dto.RatingReviewDTO;
import com.bridgelabz.bookstoreapi.entity.Book;
import com.bridgelabz.bookstoreapi.entity.ReviewAndRating;
import com.bridgelabz.bookstoreapi.entity.Seller;
import com.bridgelabz.bookstoreapi.entity.User;
import com.bridgelabz.bookstoreapi.exception.BookException;
import com.bridgelabz.bookstoreapi.exception.SellerException;
import com.bridgelabz.bookstoreapi.exception.UserException;
import com.bridgelabz.bookstoreapi.repository.BookRepository;
import com.bridgelabz.bookstoreapi.repository.ReviewRatingRepository;
import com.bridgelabz.bookstoreapi.repository.SellerRepository;
import com.bridgelabz.bookstoreapi.repository.UserRepository;
import com.bridgelabz.bookstoreapi.service.BookService;
import com.bridgelabz.bookstoreapi.utility.JWTUtil;
import com.bridgelabz.bookstoreapi.utility.MailService;
import com.bridgelabz.bookstoreapi.utility.RedisService;
import com.bridgelabz.bookstoreapi.utility.Token;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

@Service
@PropertySource("classpath:message.properties")
public class BookServiceImpl implements BookService {

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private SellerRepository sellerRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ReviewRatingRepository rrRepository;

	@Autowired
	private JWTUtil jwt;

	@Autowired
	private Environment env;

	@Autowired
	private RestHighLevelClient client;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RedisService redisService;
	
	@Autowired
	private Producer producer;

	@Autowired
	private Consumer consumer;

	@Autowired
	private MailService mailSender;
	
	public Book addBook(BookDTO bookDTO, String token) {
		Long sId = jwt.decodeToken(token);
		Book book = new Book(bookDTO);
		Seller seller = sellerRepository.findById(sId)
				.orElseThrow(() -> new SellerException(404, env.getProperty("5002")));
		List<Book> books = seller.getSellerBooks();
		boolean notExist = books.stream().noneMatch(bk -> bk.getBookName().equals(bookDTO.getBookName()));
		if (notExist) {
			System.out.println("book-->" + bookDTO.getBookAuthor());
			book.setBookApproveStatus(false);
			book.setBookVerified(false);
			book.setSellerName(seller.getSellerName());
			seller.getSellerBooks().add(book);
			bookRepository.save(book);
			sellerRepository.save(seller);

			Map<String, Object> documentMapper = objectMapper.convertValue(book, Map.class);
			IndexRequest indexRequest = new IndexRequest(Constants.INDEX, Constants.TYPE,
					String.valueOf(book.getBookId())).source(documentMapper);
			try {
				client.index(indexRequest, RequestOptions.DEFAULT);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			throw new BookException(500, env.getProperty("5001"));
		}
		return book;
	}

	@Transactional
	public void updateBook(BookDTO bookDTO, String token, Long bookId) {
		Long sId = jwt.decodeToken(token);
		Seller seller = sellerRepository.findById(sId)
				.orElseThrow(() -> new SellerException(404, env.getProperty("5002")));
		List<Book> books = seller.getSellerBooks();
		Book filteredBook = books.stream().filter(book -> book.getBookId().equals(bookId)).findFirst()
				.orElseThrow(() -> new BookException(404, env.getProperty("4041")));
		if (filteredBook.getNoOfBooks() == 0) {
			List<Long> userIds = userRepository.getUserForNotify(bookId);
			for (Long id : userIds) {
				User user = userRepository.findById(id).orElseThrow(() -> new SellerException(404, env.getProperty("5002")));
				//if (user.isPresent()) {
					mailSender.wishlistNotificationMail(user,bookId);
//					Mail mail = new Mail();
//					mail.setTo(user.get().getEmail());
//					mail.setSubject(filteredBook.getBookName()+" is available");
//					mail.setContext("Hi " + user.get().getName() + " "
//							+ Constants.BOOK_STORE__LINK + bookId);
//					producer.sendToQueue(mail);
//					consumer.receiveMail(mail);
				//}
			}
		}
		filteredBook.setBookName(bookDTO.getBookName());
		filteredBook.setBookAuthor(bookDTO.getBookAuthor());
		filteredBook.setBookDescription(bookDTO.getBookDescription());
		filteredBook.setBookPrice(bookDTO.getBookPrice());
		filteredBook.setNoOfBooks(bookDTO.getNoOfBooks());
		filteredBook.setBookUpdatedTime(LocalDateTime.now());
		filteredBook.setBookApproveStatus(false);
		filteredBook.setSellerName(seller.getSellerName());
		Book bookUpdate = bookRepository.save(filteredBook);
		sellerRepository.save(seller);
		if (bookUpdate.isBookVerified())
			updateBookInES(bookUpdate);
	}

	@Transactional
	public void deleteBook(String token, Long bookId) {
		Long sId = jwt.decodeToken(token);
		Seller seller = sellerRepository.findById(sId)
				.orElseThrow(() -> new SellerException(404, env.getProperty("5002")));
		List<Book> books = seller.getSellerBooks();
		Book filteredBook = books.stream().filter(book -> book.getBookId().equals(bookId)).findFirst()
				.orElseThrow(() -> new BookException(404, env.getProperty("4041")));

		books.remove(filteredBook);
		bookRepository.delete(filteredBook);
		sellerRepository.save(seller);
		DeleteRequest deleteRequest = new DeleteRequest(Constants.INDEX, Constants.TYPE,
				String.valueOf(filteredBook.getBookId()));
		try {
			client.delete(deleteRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeReviewAndRating(String token, RatingReviewDTO rrDTO, Long bookId) {
		Long uId = jwt.decodeToken(token);
		User user = userRepository.findById(uId).orElseThrow(() -> new UserException(404, env.getProperty("104")));
		Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookException(404, env.getProperty("4041")));
		boolean notExist = book.getReviewRating().stream()
				.noneMatch(rr -> rr.getUserName().equalsIgnoreCase(user.getName()));
		if (notExist) {
			ReviewAndRating rr = new ReviewAndRating(rrDTO);
			rr.setUserName(user.getName());
			book.getReviewRating().add(rr);
			rrRepository.save(rr);
			Book bookUpdate = bookRepository.save(book);
			if (bookUpdate.isBookVerified())
				updateBookInES(bookUpdate);
		} else {
			ReviewAndRating rr = book.getReviewRating().stream()
					.filter(r -> r.getUserName().equalsIgnoreCase(user.getName())).findFirst()
					.orElseThrow(() -> new BookException(500, env.getProperty("104")));
			rr.setRating(rrDTO.getRating());
			rr.setReview(rrDTO.getReview());
			rrRepository.save(rr);
			Book bookUpdate = bookRepository.save(book);
			if (bookUpdate.isBookVerified())
				updateBookInES(bookUpdate);
		}
	}

	public List<ReviewAndRating> getRatingsOfBook(Long bookId) {
		Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookException(404, env.getProperty("4041")));
		return book.getReviewRating();
	}

	public Object getBooks(Integer pageNo) {

		Integer start = (pageNo - 1) * 12;
		if (redisService.getBooks(pageNo) == null) {
			System.out.println("Database");
			redisService.save(bookRepository.findBook(start), pageNo);
			return redisService.getBooks(pageNo);
		} else {
			System.out.println("Redis");
			return redisService.getBooks(pageNo);
		}
//		return bookRepository.findBook(start);
	}

	@Override
	public List<Book> getAllBooks() {

		return bookRepository.findUnverifiedBook();
	}

	@Override
	public Book unApproveBook(Long bookId) {

		Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookException(404, env.getProperty("4041")));
		book.setBookApproveStatus(true);
		bookRepository.save(book);
		return book;
	}

	@Override
	public List<Book> VerifyBook(Long bookId) {

		Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookException(404, env.getProperty("4041")));
		book.setBookVerified(true);
		bookRepository.save(book);
		return bookRepository.findUnverifiedBook();
	}

	public List<Book> getBooksSortedByPriceLow(Integer pageNo) {
		Integer start = (pageNo - 1) * 12;
		return bookRepository.findBookSortedByPriceLow(start);
	}

	public List<Book> getBooksSortedByPriceHigh(Integer pageNo) {
		Integer start = (pageNo - 1) * 12;
		return bookRepository.findBookSortedByPriceHigh(start);
	}

	public List<Book> getBooksSortedByArrival(Integer pageNo) {
		Integer start = (pageNo - 1) * 12;
		return bookRepository.findBookSortedByArrival(start);
	}

	public List<Book> getBookByNameAndAuthor(String text) {
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.indices(Constants.INDEX);
		searchRequest.types(Constants.TYPE);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		QueryBuilder query = QueryBuilders.boolQuery()
				.should(QueryBuilders.queryStringQuery(text).lenient(true).field("bookName").field("bookAuthor"))
				.should(QueryBuilders.queryStringQuery("*" + text + "*").lenient(true).field("bookName")
						.field("bookAuthor"));
//				.mustNot(QueryBuilders.existsQuery(""));
		searchSourceBuilder.query(query);
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = null;
		try {
			searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return getSearchResult(searchResponse);
	}

	private List<Book> getSearchResult(SearchResponse response) {

		SearchHit[] searchHit = response.getHits().getHits();

		List<Book> books = new ArrayList<>();

		for (SearchHit hit : searchHit) {
			books.add(objectMapper.convertValue(hit.getSourceAsMap(), Book.class));
		}
		return books;
	}

	private void updateBookInES(Book filteredBook) {
		UpdateRequest updateRequest = new UpdateRequest(Constants.INDEX, Constants.TYPE,
				String.valueOf(filteredBook.getBookId()));
		updateRequest.doc(objectMapper.convertValue(filteredBook, Map.class));
		try {
			client.update(updateRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Integer getBooksCount() {
		return bookRepository.findAllBook().size();
	}

	public List<Book> getSellerBooks(String token) {
		Long sId = jwt.decodeToken(token);
		Seller seller = sellerRepository.findById(sId)
				.orElseThrow(() -> new SellerException(404, env.getProperty("5002")));
		return seller.getSellerBooks();
	}

	@Override
	public Book getBookById(Long bookId) {
		return bookRepository.findById(bookId).orElseThrow(() -> new BookException(404, env.getProperty("4041")));
	}
}
