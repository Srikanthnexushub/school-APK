import { useState } from 'react';

export function usePagination(total: number, pageSize: number) {
  const [page, setPage] = useState(1);
  const totalPages = Math.ceil(total / pageSize);

  function goToPage(p: number) {
    setPage(Math.max(1, Math.min(p, totalPages)));
  }

  return {
    page,
    setPage: goToPage,
    totalPages,
    hasNext: page < totalPages,
    hasPrev: page > 1,
    pageSize,
    offset: (page - 1) * pageSize,
  };
}
