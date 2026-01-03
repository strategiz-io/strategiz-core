# Test Runner - Deployment Checklist

This checklist covers testing and deployment procedures for the comprehensive test runner system.

## Pre-Deployment Testing

### 1. Backend Compilation ✅
- [x] All modules compile successfully
- [x] No compilation errors in controllers
- [x] No compilation errors in services
- [x] No compilation errors in repositories

### 2. Test Discovery Testing
- [ ] Run test discovery locally: `POST /v1/console/tests/discovery/refresh`
- [ ] Verify Firestore collections populated:
  - [ ] `tests/apps/{appId}` documents created
  - [ ] `tests/apps/{appId}/modules/{moduleId}` documents created
  - [ ] `tests/apps/{appId}/modules/{moduleId}/suites/{suiteId}` documents created
  - [ ] `tests/apps/{appId}/modules/{moduleId}/suites/{suiteId}/tests/{testId}` documents created
- [ ] Check app count: frontend apps (3), backend API (1), Python (1) = expected 3-5 apps
- [ ] Verify module discovery:
  - [ ] Playwright projects discovered (web, console, auth)
  - [ ] Maven modules discovered (60+ modules)
  - [ ] Pytest files discovered (2+ test files)

### 3. Backend API Endpoint Testing

#### Hierarchy Endpoints
- [ ] `GET /v1/console/tests/apps` - Returns list of test applications
- [ ] `GET /v1/console/tests/apps/{appId}` - Returns app details with modules
- [ ] `GET /v1/console/tests/apps/{appId}/modules` - Returns list of modules
- [ ] `GET /v1/console/tests/apps/{appId}/modules/{moduleId}/suites` - Returns list of suites
- [ ] `GET /v1/console/tests/apps/{appId}/modules/{moduleId}/suites/{suiteId}/tests` - Returns list of tests

#### Execution Endpoints (Can test with manual requests)
- [ ] `POST /v1/console/tests/apps/{appId}/run` - Starts app-level test execution
- [ ] `POST /v1/console/tests/apps/{appId}/modules/{moduleId}/run` - Starts module-level execution
- [ ] `POST /v1/console/tests/apps/{appId}/modules/{moduleId}/suites/{suiteId}/run` - Starts suite execution
- [ ] `POST /v1/console/tests/apps/{appId}/modules/{moduleId}/suites/{suiteId}/tests/{testId}/run` - Starts single test

#### Results Endpoints
- [ ] `GET /v1/console/tests/runs/{runId}` - Returns test run details
- [ ] `GET /v1/console/tests/runs/history` - Returns paginated history
- [ ] `GET /v1/console/tests/runs/latest-ci` - Returns latest CI/CD run
- [ ] `GET /v1/console/tests/runs/trends?timeRange=7d` - Returns trends data

#### CI Integration Endpoint
- [ ] `POST /v1/console/tests/ci/results` - Accepts CI results (test with aggregate script)

### 4. Frontend UI Testing

#### Navigation
- [ ] Sidebar shows "Test Runner" navigation item
- [ ] Clicking "Test Runner" navigates to `/tests`
- [ ] URL routing works correctly

#### Test Tree View
- [ ] Apps load and display in tree
- [ ] Expanding app shows modules
- [ ] Expanding module shows suites
- [ ] Expanding suite shows individual tests
- [ ] Icons display correctly (📱 App, 📦 Module, ✅ Suite, 🧪 Test)
- [ ] Test counts display correctly
- [ ] Estimated duration shows for each node
- [ ] Run button appears on hover for each node

#### Test Execution Panel
- [ ] Selecting a node shows execution panel
- [ ] Run button shows correct test count
- [ ] Clicking Run triggers execution
- [ ] Loading indicator appears during execution
- [ ] Results display after execution completes
- [ ] Pass/fail counts show correctly
- [ ] Duration displays in human-readable format

#### Test Results Panel
- [ ] Summary metrics display (total, passed, failed, skipped)
- [ ] Individual test results show in accordion
- [ ] Expanding test shows error messages (for failures)
- [ ] Stack traces display for failed tests
- [ ] Screenshots show for Playwright tests (if available)

#### Test History Panel
- [ ] History table loads with pagination
- [ ] Filters by selected node work correctly
- [ ] Status indicators show (✅ passed, ❌ failed)
- [ ] Pagination controls work
- [ ] Clicking row navigates to run details (if implemented)

#### Test Trends Panel
- [ ] Time range selector works (7d, 30d, 90d)
- [ ] Summary cards show metrics
- [ ] Trend chart displays correctly
- [ ] Pass rate bars render
- [ ] Daily breakdown shows correctly

### 5. Frontend Performance Testing
- [ ] Tree with 1000+ tests loads without crashing
- [ ] Lazy loading works (doesn't load all nodes at once)
- [ ] Scrolling is smooth in tree view
- [ ] Pagination handles large result sets
- [ ] No memory leaks during navigation

## Deployment Steps

### Backend Deployment

1. **Commit and Push**
   ```bash
   git status
   git push origin main
   ```

2. **Monitor Cloud Build**
   - Navigate to Google Cloud Console → Cloud Build
   - Watch build progress
   - Verify successful build

3. **Verify Cloud Run Deployment**
   ```bash
   gcloud run services describe strategiz-api --region us-east1
   ```

4. **Test Deployed Backend**
   ```bash
   curl -X GET https://api.strategiz.io/v1/console/tests/apps \
     -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
   ```

5. **Configure CI Auth Token**
   - Set `TEST_RESULTS_TOKEN` secret in GitHub Actions
   - Set `test-runner.ci.auth-token` in Vault or environment variables

### Frontend Deployment

1. **Build Console App**
   ```bash
   cd strategiz-ui
   npm run build:console
   ```

2. **Deploy to Firebase Hosting**
   ```bash
   firebase deploy --only hosting:strategiz-console
   ```

3. **Verify Deployment**
   - Navigate to https://console.strategiz.io/tests
   - Verify page loads without errors
   - Check browser console for errors

## Post-Deployment Verification

### 1. Initial Data Population
- [ ] Run test discovery from console UI or API
- [ ] Verify Firestore data populated
- [ ] Check that tree loads in UI

### 2. End-to-End Test Flow
- [ ] Navigate to console.strategiz.io/tests
- [ ] See list of applications
- [ ] Expand an application
- [ ] Expand a module
- [ ] Click "Run" on a suite
- [ ] Monitor execution in Execution panel
- [ ] View results in Results panel
- [ ] Check History panel for run record

### 3. CI/CD Integration Test
- [ ] Trigger GitHub Actions workflow manually
- [ ] Verify workflow runs successfully
- [ ] Check that test results posted to backend
- [ ] Verify results appear in Test Runner console
- [ ] Confirm CI/CD metadata (commit, branch, workflow URL) is correct

### 4. Performance & Monitoring
- [ ] Check Cloud Run logs for errors
- [ ] Monitor memory usage
- [ ] Verify no slow queries in Firestore
- [ ] Check frontend bundle size
- [ ] Verify API response times < 2s

## Rollback Plan

If critical issues are discovered:

### Backend Rollback
```bash
# Revert to previous Cloud Run revision
gcloud run services update-traffic strategiz-api \
  --to-revisions=PREVIOUS_REVISION=100 \
  --region=us-east1
```

### Frontend Rollback
```bash
# Revert to previous Firebase Hosting release
firebase hosting:rollback strategiz-console
```

### Code Rollback
```bash
# Revert commits
git revert HEAD~3..HEAD  # Revert last 3 commits
git push origin main
```

## Known Limitations (MVP)

1. **No WebSocket Streaming**: Log streaming placeholder only, not implemented
2. **No Real-Time Updates**: Execution status requires manual refresh/polling
3. **Limited Filtering**: History panel has basic filters only
4. **No Retry Mechanism**: Failed executions must be manually rerun
5. **No Concurrent Execution Limits**: Could overwhelm system with many parallel runs
6. **No Execution Queue**: Tests run immediately, no queuing for resources
7. **Basic Auth**: CI token is simple bearer token, no rotation or expiry

## Future Enhancements

- [ ] WebSocket implementation for real-time log streaming
- [ ] Execution queue and concurrency limits
- [ ] Advanced filtering and search in history
- [ ] Test result comparison (run vs run)
- [ ] Flaky test detection and reporting
- [ ] Performance regression detection
- [ ] Test coverage metrics integration
- [ ] Screenshot/video viewer for Playwright tests
- [ ] Email notifications for failures
- [ ] Slack integration for CI results

## Success Metrics

- ✅ All 3 frontend apps visible in tree
- ✅ All 60+ backend modules discoverable
- ✅ Python tests executable via console
- ✅ Test execution completes within 2x normal duration (overhead <100%)
- ✅ CI/CD results appear in console within 1 minute of workflow completion
- ✅ 90%+ uptime for test runner service
- ✅ <3 second load time for test hierarchy (with caching)

## Support

For issues or questions:
- Check Cloud Run logs: `gcloud run services logs read strategiz-api --region us-east1`
- Check Firestore data: Navigate to Firebase Console → Firestore Database
- Check GitHub Actions: Navigate to repository → Actions tab
- Report issues: Create ticket in project management system
