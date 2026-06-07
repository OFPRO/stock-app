import i18n from "i18next"
import { initReactI18next } from "react-i18next"
import LanguageDetector from "i18next-browser-languagedetector"
import fr from "./locales/fr/translation.json"
import ar from "./locales/ar/translation.json"

const savedLang = localStorage.getItem("i18nextLng")

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      fr: { translation: fr },
      ar: { translation: ar },
    },
    lng: savedLang || "fr",
    fallbackLng: "fr",
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ["localStorage", "navigator"],
      caches: ["localStorage"],
    },
  })

export const languages = [
  { code: "fr", label: "Français", dir: "ltr" },
  { code: "ar", label: "العربية", dir: "rtl" },
]

export function getCurrentDir() {
  return languages.find((l) => l.code === i18n.language)?.dir || "ltr"
}

export default i18n
