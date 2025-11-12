package customer.bookshop.handlers;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.catalogservice.Books;
import cds.gen.catalogservice.Books_;
import cds.gen.catalogservice.CatalogService_;
import cds.gen.catalogservice.OrderedBook;
import cds.gen.catalogservice.OrderedBookContext;
import cds.gen.catalogservice.SubmitOrderContext;
import cds.gen.catalogservice.SubmitOrderContext.ReturnType;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {

	@Autowired
	private PersistenceService db;

	@On
	public void submitOrder(SubmitOrderContext context) {
		// decrease and update stock in database
		db.run(Update.entity(Books_.class).byId(context.getBook()).set(b -> b.stock(), s -> s.minus(context.getQuantity())));

		// read new stock from database
		Books book = db.run(Select.from(Books_.class).where(b -> b.ID().eq(context.getBook()))).single(Books.class);

		// return new stock to client
		ReturnType result = SubmitOrderContext.ReturnType.create();
		result.setStock(book.getStock());

		OrderedBook orderedBook = OrderedBook.create();
		orderedBook.setBook(book.getId());
		orderedBook.setQuantity(context.getQuantity());
		orderedBook.setBuyer(context.getUserInfo().getName());

		OrderedBookContext orderedBookEvent = OrderedBookContext.create();
		orderedBookEvent.setData(orderedBook);
		context.getService().emit(orderedBookEvent);

		context.setResult(result);
	}

	@After(event = CqnService.EVENT_READ)
	public void discountBooks(Stream<Books> books) {
		books.filter(b -> b.getTitle() != null && b.getStock() != null)
		.filter(b -> b.getStock() > 200)
		.forEach(b -> b.setTitle(b.getTitle() + " (discounted)"));
	}

}