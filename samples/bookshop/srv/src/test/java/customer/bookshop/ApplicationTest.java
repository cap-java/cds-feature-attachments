package customer.bookshop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTest {

  @Autowired private ApplicationContext context;

  @Test
  void checkApplicationContextCanBeLoaded() {
    assertThat(context).isNotNull();
  }
}