using test.data.model as db from '../db/data-model';

service TestService {
    entity Roots as projection on db.Roots;
}

service TestDraftService {
    @odata.draft.enabled
    entity Roots as projection on db.Roots;
}
