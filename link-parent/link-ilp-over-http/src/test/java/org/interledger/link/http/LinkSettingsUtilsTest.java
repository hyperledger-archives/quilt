package org.interledger.link.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LinkSettingsUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ImmutableMap<String, Object> flattenedSettings = ImmutableMap.<String, Object>builder()
      .put("parent.name", "foo")
      .put("parent.age", 100)
      .put("parent.son.name", "bar")
      .put("parent.son.age", 50)
      .put("parent.son.grandson.name", "fizz")
      .put("parent.son.grandson.age", 15)
      .put("parent.son.granddaughter.name", "buzz")
      .put("parent.son.granddaughter.age", 20)
      .build();

  @Test
  public void flattenSettingsAlreadyFlattened() {
    assertThat(LinkSettingsUtils.flattenSettings(flattenedSettings)).isEqualTo(flattenedSettings);
  }

  @Test
  public void flattenSettings() {
    Map<String, Object> grandsonSettings = ImmutableMap.of("name", "fizz", "age", 15);
    Map<String, Object> grandDaughterSettings = ImmutableMap.of("name", "buzz", "age", 20);
    Map<String, Object> sonSettings = ImmutableMap.of("name", "bar", "age", 50,
        "grandson", grandsonSettings,
        "granddaughter", grandDaughterSettings);
    Map<String, Object> parentSettings = ImmutableMap.of("name", "foo", "age", 100,
        "son", sonSettings);

    Map<String, Object> settings = ImmutableMap.of("parent", parentSettings);

    assertThat(LinkSettingsUtils.flattenSettings(settings)).isEqualTo(flattenedSettings);
  }

  @Test
  public void getAuthTypeWithSimple() {
    assertThat(LinkSettingsUtils.getAuthType(AbstractHttpLinkSettingsTest.customSettingsSimpleFlat()))
        .isEqualTo(Optional.of(IlpOverHttpLinkSettings.AuthType.SIMPLE));
    assertThat(LinkSettingsUtils.getAuthType(AbstractHttpLinkSettingsTest.customSettingsSimpleHierarchical()))
        .isEqualTo(Optional.of(IlpOverHttpLinkSettings.AuthType.SIMPLE));
  }

  @Test
  public void getAuthTypeWithJwt() {
    assertThat(LinkSettingsUtils.getAuthType(AbstractHttpLinkSettingsTest.customSettingsJwtFlat()))
        .isEqualTo(Optional.of(IlpOverHttpLinkSettings.AuthType.JWT));
    assertThat(LinkSettingsUtils.getAuthType(AbstractHttpLinkSettingsTest.customSettingsJwtHierarchical()))
        .isEqualTo(Optional.of(IlpOverHttpLinkSettings.AuthType.JWT));
  }

  @Test
  public void getAuthTypeWithMissing() {
    assertThat(LinkSettingsUtils.getAuthType(new HashMap<>())).isEqualTo(Optional.empty());
  }

  @Test
  public void getAuthTypeWithDuplicate() {
    Map<String, Object> tooManyAuthTypes = new HashMap<>();
    tooManyAuthTypes.putAll(AbstractHttpLinkSettingsTest.customSettingsSimpleFlat());
    tooManyAuthTypes.putAll(AbstractHttpLinkSettingsTest.customSettingsJwtFlat());

    expectedException.expect(IllegalArgumentException.class);
    LinkSettingsUtils.getAuthType(tooManyAuthTypes);
  }





}