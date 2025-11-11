package customer.bookshop.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.request.UserInfo;

import cds.gen.catalogservice.Books;
import cds.gen.catalogservice.CatalogService;
import cds.gen.catalogservice.OrderedBookContext;
import cds.gen.catalogservice.SubmitOrderContext;

class CatalogServiceHandlerTest {

	@Mock
	private PersistenceService db;

	@InjectMocks
	private CatalogServiceHandler handler;

	private Books book = Books.create();

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		book.setTitle("title");
	}

	@Test
	void testDiscount() {
		book.setStock(500);
		handler.discountBooks(Stream.of(book));
		assertEquals("title (discounted)", book.getTitle());
	}

	@Test
	void testNoDiscount() {
		book.setStock(100);
		handler.discountBooks(Stream.of(book));
		assertEquals("title", book.getTitle());
	}

	@Test
	void testNoStockAvailable() {
		handler.discountBooks(Stream.of(book));
		assertEquals("title", book.getTitle());
	}

	@Test
	void testDiscountWithNullTitle() {
		book.setTitle(null);
		book.setStock(500);
		handler.discountBooks(Stream.of(book));
		// Should not throw exception and title should remain null
		assertEquals(null, book.getTitle());
	}

	@Test
	void testDiscountWithNullStock() {
		book.setTitle("test");
		book.setStock(null);
		handler.discountBooks(Stream.of(book));
		// Should not throw exception and title should remain unchanged
		assertEquals("test", book.getTitle());
	}

	@Test
	void testSubmitOrder() {
		// Setup
		String bookId = "aeeda49f-72f2-4880-be27-a513b2e53040";
		Integer quantity = 2;
		Integer expectedNewStock = 9;
		String userName = "testuser";

		// Mock the context
		SubmitOrderContext context = mock(SubmitOrderContext.class);
		UserInfo userInfo = mock(UserInfo.class);

		// Import the generated service interface
		CatalogService catalogService = mock(CatalogService.class);

		when(context.getBook()).thenReturn(bookId);
		when(context.getQuantity()).thenReturn(quantity);
		when(context.getUserInfo()).thenReturn(userInfo);
		when(context.getService()).thenReturn(catalogService);
		when(userInfo.getName()).thenReturn(userName);

		// Mock the book result after update
		Books updatedBook = Books.create();
		updatedBook.setId(bookId);
		updatedBook.setStock(expectedNewStock);

		Result mockResult = mock(Result.class);
		when(mockResult.single(Books.class)).thenReturn(updatedBook);
		when(db.run(any(Select.class))).thenReturn(mockResult);

		// Execute
		handler.submitOrder(context);

		// Verify database operations
		verify(db).run(any(Update.class));
		verify(db).run(any(Select.class));
		verify(context).setResult(any(SubmitOrderContext.ReturnType.class));
		verify(catalogService).emit(any(OrderedBookContext.class));
	}

	@Test
	void testSubmitOrderWithZeroQuantity() {
		// Setup
		String bookId = "book-123";
		Integer quantity = 0;
		Integer currentStock = 100;
		String userName = "testuser";

		// Mock the context
		SubmitOrderContext context = mock(SubmitOrderContext.class);
		UserInfo userInfo = mock(UserInfo.class);

		CatalogService catalogService = mock(CatalogService.class);

		when(context.getBook()).thenReturn(bookId);
		when(context.getQuantity()).thenReturn(quantity);
		when(context.getUserInfo()).thenReturn(userInfo);
		when(context.getService()).thenReturn(catalogService);
		when(userInfo.getName()).thenReturn(userName);

		// Mock the book result (stock should remain the same)
		Books updatedBook = Books.create();
		updatedBook.setId(bookId);
		updatedBook.setStock(currentStock);

		Result mockResult = mock(Result.class);
		when(mockResult.single(Books.class)).thenReturn(updatedBook);
		when(db.run(any(Select.class))).thenReturn(mockResult);

		// Execute
		handler.submitOrder(context);

		// Verify database operations still happen
		verify(db).run(any(Update.class));
		verify(db).run(any(Select.class));
		verify(context).setResult(any(SubmitOrderContext.ReturnType.class));
	}
}
