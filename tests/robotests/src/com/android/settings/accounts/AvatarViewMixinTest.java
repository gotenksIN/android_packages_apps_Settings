/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.accounts.Account;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.widget.ImageView;

import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(SettingsRobolectricTestRunner.class)
public class AvatarViewMixinTest {
    private static final String DUMMY_ACCOUNT = "test@domain.com";
    private static final String DUMMY_DOMAIN = "domain.com";
    private static final String DUMMY_AUTHORITY = "authority.domain.com";

    private Context mContext;
    private ImageView mImageView;
    private ActivityController mController;
    private SettingsHomepageActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mImageView = new ImageView(mContext);
        mController = Robolectric.buildActivity(SettingsHomepageActivity.class).create();
        mActivity = (SettingsHomepageActivity) mController.get();
    }

    @Test
    public void hasAccount_useDefaultAccountData_returnFalse() {
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(mActivity, mImageView);
        assertThat(avatarViewMixin.hasAccount()).isFalse();
    }

    @Test
    @Config(shadows = ShadowAccountFeatureProviderImpl.class)
    public void hasAccount_useShadowAccountData_returnTrue() {
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(mActivity, mImageView);
        assertThat(avatarViewMixin.hasAccount()).isTrue();
    }

    @Test
    public void onStart_configDisabled_doNothing() {
        final AvatarViewMixin mixin = spy(new AvatarViewMixin(mActivity, mImageView));
        mixin.onStart();

        verify(mixin, never()).hasAccount();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void onStart_useMockAvatarViewMixin_shouldBeExecuted() {
        final AvatarViewMixin mockAvatar = spy(new AvatarViewMixin(mActivity, mImageView));

        mActivity.getLifecycle().addObserver(mockAvatar);
        mController.start();

        verify(mockAvatar).hasAccount();
    }

    @Test
    public void queryProviderAuthority_useShadowPackagteManager_returnNull() {
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(mActivity, mImageView);

        assertThat(avatarViewMixin.queryProviderAuthority()).isNull();
    }

    @Test
    public void queryProviderAuthority_useNewShadowPackagteManager_returnAuthority() {
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(mActivity, mImageView);
        ShadowPackageManager shadowPackageManager = Shadow.extract(mContext.getPackageManager());
        final PackageInfo accountProvider = new PackageInfo();
        accountProvider.packageName = "test.pkg";
        accountProvider.applicationInfo = new ApplicationInfo();
        accountProvider.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        accountProvider.applicationInfo.packageName = accountProvider.packageName;
        accountProvider.providers = new ProviderInfo[1];
        accountProvider.providers[0] = new ProviderInfo();
        accountProvider.providers[0].authority = DUMMY_AUTHORITY;
        accountProvider.providers[0].packageName = accountProvider.packageName;
        accountProvider.providers[0].name = "test.class";
        accountProvider.providers[0].applicationInfo = accountProvider.applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.providerInfo = accountProvider.providers[0];
        shadowPackageManager.addResolveInfoForIntent(AvatarViewMixin.INTENT_GET_ACCOUNT_DATA,
                resolveInfo);
        assertThat(avatarViewMixin.queryProviderAuthority()).isEqualTo(DUMMY_AUTHORITY);
    }

    @Implements(value = AccountFeatureProviderImpl.class)
    public static class ShadowAccountFeatureProviderImpl {

        @Implementation
        public Account[] getAccounts(Context context) {
            Account[] accounts = {new Account(DUMMY_ACCOUNT, DUMMY_DOMAIN)};
            return accounts;
        }
    }
}
