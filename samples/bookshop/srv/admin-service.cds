using {sap.capire.bookshop as my} from '../db/schema';

service AdminService @(requires: 'admin') {
  entity Books   as projection on my.Books;
  entity Authors as projection on my.Authors;
}

service NonDraftAdminService @(requires: 'admin') {
  @odata.draft.enabled: false
  entity Books   as projection on my.Books;
  entity Authors as projection on my.Authors;
}
