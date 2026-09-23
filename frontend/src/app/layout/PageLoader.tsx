import { Center, Loader } from '@mantine/core';

/** Shown while the first route module (a lazily loaded page chunk) is being fetched. */
export function PageLoader() {
  return (
    <Center mih="50vh">
      <Loader aria-label="Carregando" />
    </Center>
  );
}
