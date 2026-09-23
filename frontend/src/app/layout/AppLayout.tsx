import { AppShell, Burger, Group, NavLink, Progress, Text, ThemeIcon, Title } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import {
  IconArrowsExchange,
  IconBuildingBank,
  IconCalculator,
  IconListDetails,
} from '@tabler/icons-react';
import type { ComponentType } from 'react';
import { NavLink as RouterNavLink, Outlet, useLocation, useNavigation } from 'react-router';

import { paths } from '../paths';

interface NavItem {
  to: string;
  label: string;
  description: string;
  icon: ComponentType<{ size?: number; stroke?: number }>;
  end?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  {
    to: paths.operatorPanel,
    label: 'Painel do operador',
    description: 'Simular e ceder recebíveis',
    icon: IconCalculator,
    end: true,
  },
  {
    to: paths.transactions,
    label: 'Transações',
    description: 'Extrato de liquidação',
    icon: IconListDetails,
  },
  {
    to: paths.exchangeRates,
    label: 'Câmbio',
    description: 'Taxas e caixa do fundo',
    icon: IconArrowsExchange,
  },
];

export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure(false);
  const navigation = useNavigation();
  const { pathname } = useLocation();

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 260, breakpoint: 'sm', collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        {navigation.state === 'loading' ? (
          <Progress
            value={100}
            animated
            size="xs"
            radius={0}
            pos="absolute"
            top={0}
            left={0}
            right={0}
            aria-label="Carregando página"
          />
        ) : null}
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              opened={opened}
              onClick={toggle}
              hiddenFrom="sm"
              size="sm"
              aria-label={opened ? 'Fechar menu' : 'Abrir menu'}
            />
            <ThemeIcon size="lg" radius="md" variant="filled" aria-hidden>
              <IconBuildingBank size={20} />
            </ThemeIcon>
            <div>
              <Title order={1} size="h4" lh={1.1}>
                SRM Credit Engine
              </Title>
              <Text size="xs" c="dimmed" visibleFrom="xs">
                Cessão de crédito multimoedas
              </Text>
            </div>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs" aria-label="Navegação principal">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            component={RouterNavLink}
            to={item.to}
            end={item.end}
            active={item.end ? pathname === item.to : pathname.startsWith(item.to)}
            label={item.label}
            description={item.description}
            leftSection={<item.icon size={20} stroke={1.6} />}
            onClick={close}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
