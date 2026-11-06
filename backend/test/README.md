# Test

Service tests with fakes, repository tests against in-memory H2, and route tests with `testApplication`. Run them
with `./kotlin test` from the repository root.

`TestFixtures.kt` holds the shared helpers (`testConfig()`, `TestServices`, `configureTestApp()`, `register()`, ...).
Route tests call `configureTestApp(TestServices())`, never `module()`, which needs a database. Explained in
`docs/guide/11-testing.md`.
