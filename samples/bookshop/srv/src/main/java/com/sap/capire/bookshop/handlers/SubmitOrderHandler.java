package com.sap.capire.bookshop.handlers;

import org.springframework.stereotype.Component;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import cds.gen.catalogservice.Books;
import cds.gen.catalogservice.Books_;
import cds.gen.catalogservice.CatalogService_;
import cds.gen.catalogservice.SubmitOrderContext;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class SubmitOrderHandler implements EventHandler {

  private final PersistenceService persistenceService;

  public SubmitOrderHandler(PersistenceService persistenceService) {
    this.persistenceService = persistenceService;
  }

  @On
  public void onSubmitOrder(SubmitOrderContext context) {
    Select<Books_> byId = Select.from(cds.gen.catalogservice.Books_.class).byId(context.getBook());
Books book = persistenceService.run(byId).single(Books.class);
    if (context.getQuantity() > book.getStock())
      throw new IllegalArgumentException(context.getQuantity() + " exceeds stock for book #" + book.getTitle());
    book.setStock(book.getStock() - context.getQuantity());

    persistenceService.run(Update.entity(Books_.CDS_NAME).data(book));

    context.setCompleted();
  }
}