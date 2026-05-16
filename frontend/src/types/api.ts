export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  total: number;
  list: T[];
  page: number;
  pageSize: number;
}
