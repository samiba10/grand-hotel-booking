import { Link } from 'react-router-dom';
import { useI18n } from '../i18n/I18nProvider';
import { PageHeader } from '../ui/components';

export function NotFoundPage() {
  const { t } = useI18n();
  return (
    <>
      <PageHeader title={t('notFound.title')} />
      <Link to="/search" className="text-brand underline">
        {t('nav.search')}
      </Link>
    </>
  );
}
