package org.medicmobile.webapp.mobile;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.medicmobile.webapp.mobile.RequestStoragePermissionActivity.TRIGGER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.robolectric.RuntimeEnvironment.getApplication;
import static org.robolectric.Shadows.shadowOf;

import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplicationPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class RequestStoragePermissionActivityTest {

	@Rule
	public ActivityScenarioRule<RequestStoragePermissionActivity> scenarioRule = new ActivityScenarioRule<>(RequestStoragePermissionActivity.class);

	private ShadowApplicationPackageManager packageManager;

	@Before
	public void setup() {
		packageManager = (ShadowApplicationPackageManager) shadowOf(getApplication().getPackageManager());
	}

	@Test
	public void onClickAllow_withPermissionGranted_setResolveOk() {
		try(MockedStatic<MedicLog> medicLogMock = mockStatic(MedicLog.class)) {
			ActivityScenario<RequestStoragePermissionActivity> scenario = scenarioRule.getScenario();

			scenario.onActivity(requestStoragePermissionActivity -> {
				//> GIVEN
				requestStoragePermissionActivity.getIntent().putExtra(TRIGGER_CLASS, "a.trigger.class");
				ShadowActivity shadowActivity = shadowOf(requestStoragePermissionActivity);
				shadowActivity.grantPermissions(READ_EXTERNAL_STORAGE);

				//> WHEN
				requestStoragePermissionActivity.onClickAllow(null);
			});
			scenario.moveToState(Lifecycle.State.DESTROYED);

			//> THEN
			ActivityResult result = scenario.getResult();
			assertEquals(RESULT_OK, result.getResultCode());
			Intent resultIntent = result.getResultData();
			assertNotNull(resultIntent);
			assertEquals("a.trigger.class", resultIntent.getStringExtra(TRIGGER_CLASS));

			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User agree with prominent disclosure message.")
			));
			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User allowed storage permission.")
			));
		}
	}

	@Test
	public void onClickAllow_withoutExtras_setTriggerClassNull() {
		try(MockedStatic<MedicLog> medicLogMock = mockStatic(MedicLog.class)) {
			ActivityScenario<RequestStoragePermissionActivity> scenario = scenarioRule.getScenario();

			scenario.onActivity(requestStoragePermissionActivity -> {
				//> GIVEN
				ShadowActivity shadowActivity = shadowOf(requestStoragePermissionActivity);
				shadowActivity.grantPermissions(READ_EXTERNAL_STORAGE);

				//> WHEN
				requestStoragePermissionActivity.onClickAllow(null);
			});
			scenario.moveToState(Lifecycle.State.DESTROYED);

			//> THEN
			ActivityResult result = scenario.getResult();
			assertEquals(RESULT_OK, result.getResultCode());
			Intent resultIntent = result.getResultData();
			assertNotNull(resultIntent);
			assertNull(resultIntent.getStringExtra(TRIGGER_CLASS));

			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User agree with prominent disclosure message.")
			));
			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User allowed storage permission.")
			));
		}
	}

	@Test
	public void onClickAllow_withPermissionDenied_setResolveCanceled() {
		try(MockedStatic<MedicLog> medicLogMock = mockStatic(MedicLog.class)) {
			ActivityScenario<RequestStoragePermissionActivity> scenario = scenarioRule.getScenario();

			scenario.onActivity(requestStoragePermissionActivity -> {
				Intents.init();

				//> GIVEN
				requestStoragePermissionActivity.getIntent().putExtra(TRIGGER_CLASS, "a.trigger.class");
				ShadowActivity shadowActivity = shadowOf(requestStoragePermissionActivity);
				packageManager.setShouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE, true);

				//> WHEN
				requestStoragePermissionActivity.onClickAllow(null);
				Intent permissionIntent = shadowActivity.peekNextStartedActivityForResult().intent;
				shadowActivity.receiveResult(permissionIntent, RESULT_OK, null);

				//> THEN
				assertEquals("android.content.pm.action.REQUEST_PERMISSIONS", permissionIntent.getAction());
				Bundle extras = permissionIntent.getExtras();
				assertNotNull(extras);
				String[] permissions = extras.getStringArray("android.content.pm.extra.REQUEST_PERMISSIONS_NAMES");
				assertEquals(1, permissions.length);
				assertEquals(READ_EXTERNAL_STORAGE, permissions[0]);

				Intents.release();
			});

			ActivityResult result = scenario.getResult();
			assertEquals(RESULT_CANCELED, result.getResultCode());
			Intent resultIntent = result.getResultData();
			assertNotNull(resultIntent);
			assertEquals("a.trigger.class", resultIntent.getStringExtra(TRIGGER_CLASS));

			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User agree with prominent disclosure message.")
			));
			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User rejected storage permission.")
			));
		}
	}

	@Test
	public void onClickAllow_withNeverAskAgainAndPermissionGranted_setResolveOk() {
		try(MockedStatic<MedicLog> medicLogMock = mockStatic(MedicLog.class)) {
			ActivityScenario<RequestStoragePermissionActivity> scenario = scenarioRule.getScenario();

			scenario.onActivity(requestStoragePermissionActivity -> {
				Intents.init();

				//> GIVEN
				String packageName = "package:" + requestStoragePermissionActivity.getPackageName();
				requestStoragePermissionActivity.getIntent().putExtra(TRIGGER_CLASS, "a.trigger.class");
				ShadowActivity shadowActivity = shadowOf(requestStoragePermissionActivity);
				// Setting "Never ask again" case.
				packageManager.setShouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE, false);

				//> WHEN
				requestStoragePermissionActivity.onClickAllow(null);
				Intent permissionIntent = shadowActivity.peekNextStartedActivityForResult().intent;
				shadowActivity.receiveResult(permissionIntent, RESULT_OK, null);

				shadowActivity.grantPermissions(READ_EXTERNAL_STORAGE);
				Intent settingsIntent = shadowActivity.peekNextStartedActivityForResult().intent;
				shadowActivity.receiveResult(settingsIntent, RESULT_OK, null);

				//> THEN
				assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, settingsIntent.getAction());
				assertEquals(packageName, settingsIntent.getData().toString());

				Intents.release();
			});

			ActivityResult result = scenario.getResult();
			assertEquals(RESULT_OK, result.getResultCode());
			Intent resultIntent = result.getResultData();
			assertNotNull(resultIntent);
			assertEquals("a.trigger.class", resultIntent.getStringExtra(TRIGGER_CLASS));

			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User agree with prominent disclosure message.")
			));
			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User rejected storage permission twice or has selected \"never ask again\"." +
					" Sending user to the app's setting to manually grant the permission.")
			));
			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User granted storage permission from app's settings.")
			));
		}
	}

	@Test
	public void onClickAllow_withNeverAskAgainAndPermissionDenied_setResolveCanceled() {
		try(MockedStatic<MedicLog> medicLogMock = mockStatic(MedicLog.class)) {
			ActivityScenario<RequestStoragePermissionActivity> scenario = scenarioRule.getScenario();

			scenario.onActivity(requestStoragePermissionActivity -> {
				Intents.init();

				//> GIVEN
				String packageName = "package:" + requestStoragePermissionActivity.getPackageName();
				requestStoragePermissionActivity.getIntent().putExtra(TRIGGER_CLASS, "a.trigger.class");
				ShadowActivity shadowActivity = shadowOf(requestStoragePermissionActivity);
				// Setting "Never ask again" case.
				packageManager.setShouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE, false);

				//> WHEN
				requestStoragePermissionActivity.onClickAllow(null);
				Intent permissionIntent = shadowActivity.peekNextStartedActivityForResult().intent;
				shadowActivity.receiveResult(permissionIntent, RESULT_OK, null);

				Intent settingsIntent = shadowActivity.peekNextStartedActivityForResult().intent;
				shadowActivity.receiveResult(settingsIntent, RESULT_OK, null);

				//> THEN
				assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, settingsIntent.getAction());
				assertEquals(packageName, settingsIntent.getData().toString());

				Intents.release();
			});

			ActivityResult result = scenario.getResult();
			assertEquals(RESULT_CANCELED, result.getResultCode());
			Intent resultIntent = result.getResultData();
			assertNotNull(resultIntent);
			assertEquals("a.trigger.class", resultIntent.getStringExtra(TRIGGER_CLASS));

			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User agree with prominent disclosure message.")
			));
			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User rejected storage permission twice or has selected \"never ask again\"." +
					" Sending user to the app's setting to manually grant the permission.")
			));
			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User didn't grant storage permission from app's settings.")
			));
		}
	}

	@Test
	public void onClickNegative_noIntentsStarted_setResolveCanceled() {
		try(MockedStatic<MedicLog> medicLogMock = mockStatic(MedicLog.class)) {
			ActivityScenario<RequestStoragePermissionActivity> scenario = scenarioRule.getScenario();

			scenario.onActivity(requestStoragePermissionActivity -> {
				Intents.init();
				//> WHEN
				requestStoragePermissionActivity.onClickDeny(null);

				//> THEN
				assertEquals(0, Intents.getIntents().size());

				Intents.release();
			});
			scenario.moveToState(Lifecycle.State.DESTROYED);

			ActivityResult result = scenario.getResult();
			assertEquals(RESULT_CANCELED, result.getResultCode());
			Intent resultIntent = result.getResultData();
			assertNotNull(resultIntent);
			assertNull(resultIntent.getStringExtra(TRIGGER_CLASS));

			medicLogMock.verify(() -> MedicLog.trace(
				any(RequestStoragePermissionActivity.class),
				eq("RequestStoragePermissionActivity :: User disagree with prominent disclosure message.")
			));
		}
	}
}
