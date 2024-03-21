using test.data.model as db from '../db/data-model';

service TestService {
    entity Roots as projection on db.Roots;
    entity AttachmentEntity as projection on db.AttachmentEntity;
}

service TestDraftService {
    @odata.draft.enabled
    entity DraftRoots as projection on db.Roots;
}
