import React, { useEffect } from 'react';

import { view } from 'react-easy-state';
import { LightDarkSwitch } from "../../../carrotsearch/ui/LightDarkSwitch.js";
import { persistentStore } from "../../util/persistent-store.js";

export const themeStore = persistentStore("uiConfig",
  {
    theme: "dark"
  },
  {
    flipTheme: () => themeStore.theme = themeStore.isDarkTheme() ? "light" : "dark",
    isDarkTheme: () => themeStore.theme === "dark"
  });


function ThemeSwitchImpl () {
  function updateTheme() {
    const classList = document.body.classList;
    if (themeStore.isDarkTheme()) {
      classList.remove("light");
      classList.add("bp3-dark", "dark");
    } else {
      classList.remove("bp3-dark", "dark");
      classList.add("light");
    }
  }

  function flipTheme() {
    themeStore.flipTheme();
    updateTheme();
  }

  // Set theme on initial render.
  useEffect(function () {
    updateTheme();
  }, []);

  const isDarkTheme = themeStore.isDarkTheme();
  return (
    <LightDarkSwitch dark={isDarkTheme} onChange={flipTheme} />
  );
}

export const ThemeSwitch = view(ThemeSwitchImpl);
