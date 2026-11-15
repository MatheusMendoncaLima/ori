import type { Metadata } from "next";
import {
  LegalPage,
  legalStaticParams,
  legalTitle,
} from "../../legal/LegalPage";

export const dynamicParams = false;

export function generateStaticParams() {
  return legalStaticParams();
}

export async function generateMetadata({
  params,
}: PageProps<"/[lang]/terms">): Promise<Metadata> {
  const { lang } = await params;
  return { title: legalTitle(lang, "terms") };
}

export default async function Page({ params }: PageProps<"/[lang]/terms">) {
  const { lang } = await params;
  return <LegalPage lang={lang} kind="terms" />;
}
