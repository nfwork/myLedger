import { dbfoundPost, unwrapDbfound } from '../core/dbfound'

/** @see model user/user.xml — model 名 `user/user` */

export async function registerAccount(body) {
  const data = await dbfoundPost('/user/user.execute!register', body)
  unwrapDbfound(data)
  return data
}

export async function updateProfile(body) {
  const data = await dbfoundPost('/user/user.execute!updateProfile', body)
  unwrapDbfound(data)
  return data
}

export async function changePassword(body) {
  const data = await dbfoundPost('/user/user.execute!changePassword', body)
  unwrapDbfound(data)
  return data
}
