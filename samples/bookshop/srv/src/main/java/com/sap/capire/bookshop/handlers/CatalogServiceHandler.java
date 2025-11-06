package com.sap.capire.bookshop.handlers;

import java.util.List;
import org.springframework.stereotype.Component;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import cds.gen.catalogservice.Books;
import cds.gen.catalogservice.Books_;
import cds.gen.catalogservice.CatalogService_;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {
  @After(event = CqnService.EVENT_READ, entity = Books_.CDS_NAME)
  public void addDiscountIfApplicable(List<Books> books) {
    for (Books book : books) {
      if (book.getStock() != null && book.getStock() > 111) {
        book.setTitle(book.getTitle() + " -- 11% discount!");
      }
    }
  }
}