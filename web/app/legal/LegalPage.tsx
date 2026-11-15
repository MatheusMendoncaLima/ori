import Link from "next/link";
import { notFound } from "next/navigation";
import { LANGUAGES, isLanguage, legalTexts, type Language } from "./content";

export type DocumentKind = "privacy" | "terms" | "deletion";

const paths: Record<DocumentKind, string> = {
  privacy: "privacy",
  terms: "terms",
  deletion: "delete-account",
};

const languageNames: Record<Language, string> = {
  pt: "Português",
  en: "English",
  es: "Español",
};

/** Every legal page exists in the three languages of the app, built ahead of time. */
export function legalStaticParams() {
  return LANGUAGES.map((lang) => ({ lang }));
}

export function legalTitle(lang: string, kind: DocumentKind): string {
  return isLanguage(lang) ? legalTexts[lang][kind].title : "ORI";
}

/** One legal document: title, version, intro and numbered sections, with links to the other languages. */
export function LegalPage({
  lang,
  kind,
}: {
  lang: string;
  kind: DocumentKind;
}) {
  if (!isLanguage(lang)) notFound();
  const texts = legalTexts[lang];
  const document = texts[kind];
  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10 text-[#102a45] leading-relaxed">
      <h1 className="text-3xl font-bold">{document.title}</h1>
      <p className="mt-1 text-sm text-[#546e85]">{texts.updated}</p>
      <p className="mt-6">{document.intro}</p>
      {document.sections.map((section) => (
        <section key={section.title} className="mt-8">
          <h2 className="text-xl font-semibold">{section.title}</h2>
          {section.paragraphs.map((paragraph) => (
            <p key={paragraph} className="mt-3">
              {paragraph}
            </p>
          ))}
        </section>
      ))}
      <nav className="mt-12 border-t border-[#c3d5e3] pt-4 text-sm">
        <span className="text-[#546e85]">{texts.otherLanguages}: </span>
        {LANGUAGES.filter((other) => other !== lang).map((other) => (
          <Link
            key={other}
            href={`/${other}/${paths[kind]}`}
            className="mr-4 text-[#0f6e72] underline"
          >
            {languageNames[other]}
          </Link>
        ))}
      </nav>
    </main>
  );
}
