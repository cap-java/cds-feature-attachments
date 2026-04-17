namespace unit.test;

using unit.test as db from './db-model';

service TestService {
    @odata.draft.enabled
    entity RootTable as projection on db.Roots;

    @odata.draft.enabled
    entity InlineOnlyTable as projection on db.InlineOnly;
}
